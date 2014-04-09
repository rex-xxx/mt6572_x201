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

package com.android.exchange.adapter;

import android.content.ContentUris;
import android.content.ContentValues;
import android.test.suitebuilder.annotation.SmallTest;

import com.android.emailcommon.Configuration;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.EmailContent.Attachment;
import com.android.emailcommon.provider.EmailContent.Body;
import com.android.emailcommon.provider.EmailContent.Message;
import com.android.emailcommon.provider.EmailContent.MessageColumns;
import com.android.emailcommon.provider.EmailContent.SyncColumns;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.service.EmailServiceStatus;
import com.android.exchange.CommandStatusException;
import com.android.exchange.Eas;
import com.android.exchange.EasResponse;
import com.android.exchange.EasSyncService;
import com.android.exchange.adapter.EmailSyncAdapter;
import com.android.exchange.adapter.Serializer;
import com.android.exchange.adapter.Tags;
import com.android.exchange.adapter.EmailSyncAdapter.EasEmailSyncParser;
import com.android.exchange.adapter.EmailSyncAdapter.EasEmailSyncParser.ServerChange;
import com.android.exchange.provider.EmailContentSetupUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.TimeZone;

@SmallTest
public class EmailSyncAdapterTests extends SyncAdapterTestCase<EmailSyncAdapter> {

    private static final String WHERE_ACCOUNT_KEY = Message.ACCOUNT_KEY + "=?";
    private static final String[] ACCOUNT_ARGUMENT = new String[1];

    // A server id that is guaranteed to be test-related
    private static final String TEST_SERVER_ID = "__1:22";

    public EmailSyncAdapterTests() {
        super();
    }

    /**
     * Check functionality for getting mime type from a file name (using its extension)
     * The default for all unknown files is application/octet-stream
     */
    public void testGetMimeTypeFromFileName() throws IOException {
        EasSyncService service = getTestService();
        EmailSyncAdapter adapter = new EmailSyncAdapter(service);
        EasEmailSyncParser p = adapter.new EasEmailSyncParser(getTestInputStream(), adapter);
        // Test a few known types
        String mimeType = p.getMimeTypeFromFileName("foo.jpg");
        assertEquals("image/jpeg", mimeType);
        // Make sure this is case insensitive
        mimeType = p.getMimeTypeFromFileName("foo.JPG");
        assertEquals("image/jpeg", mimeType);
        mimeType = p.getMimeTypeFromFileName("this_is_a_weird_filename.gif");
        assertEquals("image/gif", mimeType);
        // Test an illegal file name ending with the extension prefix
        mimeType = p.getMimeTypeFromFileName("foo.");
        assertEquals("application/octet-stream", mimeType);
        // Test a really awful name
        mimeType = p.getMimeTypeFromFileName(".....");
        assertEquals("application/octet-stream", mimeType);
        // Test a bare file name (no extension)
        mimeType = p.getMimeTypeFromFileName("foo");
        assertEquals("application/octet-stream", mimeType);
        // And no name at all (null isn't a valid input)
        mimeType = p.getMimeTypeFromFileName("");
        assertEquals("application/octet-stream", mimeType);
    }

    public void testFormatDateTime() throws IOException {
        EmailSyncAdapter adapter = getTestSyncAdapter(EmailSyncAdapter.class);
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        // Calendar is odd, months are zero based, so the first 11 below is December...
        calendar.set(2008, 11, 11, 18, 19, 20);
        String date = adapter.formatDateTime(calendar);
        assertEquals("2008-12-11T18:19:20.000Z", date);
        calendar.clear();
        calendar.set(2012, 0, 2, 23, 0, 1);
        date = adapter.formatDateTime(calendar);
        assertEquals("2012-01-02T23:00:01.000Z", date);
    }

    public void testSendDeletedItems() throws IOException {
        setupAccountMailboxAndMessages(0);
        // Setup our adapter and parser
        setupSyncParserAndAdapter(mAccount, mMailbox);

        Serializer s = new Serializer();
        ArrayList<Long> ids = new ArrayList<Long>();
        ArrayList<Long> deletedIds = new ArrayList<Long>();

        // Create account and two mailboxes
        mSyncAdapter.mAccount = mAccount;
        Mailbox box1 = EmailContentSetupUtils.setupMailbox("box1", mAccount.mId, true,
                mProviderContext);
        mSyncAdapter.mMailbox = box1;

        // Create 3 messages
        Message msg1 = EmailContentSetupUtils.setupMessage("message1", mAccount.mId, box1.mId,
                true, true, mProviderContext);
        ids.add(msg1.mId);
        Message msg2 = EmailContentSetupUtils.setupMessage("message2", mAccount.mId, box1.mId,
                true, true, mProviderContext);
        ids.add(msg2.mId);
        Message msg3 = EmailContentSetupUtils.setupMessage("message3", mAccount.mId, box1.mId,
                true, true, mProviderContext);
        ids.add(msg3.mId);
        assertEquals(3, EmailContent.count(mProviderContext, Message.CONTENT_URI, WHERE_ACCOUNT_KEY,
                getAccountArgument(mAccount.mId)));

        // Delete them
        for (long id: ids) {
            mResolver.delete(ContentUris.withAppendedId(Message.SYNCED_CONTENT_URI, id),
                    null, null);
        }

        // Confirm that the messages are in the proper table
        assertEquals(0, EmailContent.count(mProviderContext, Message.CONTENT_URI, WHERE_ACCOUNT_KEY,
                getAccountArgument(mAccount.mId)));
        assertEquals(3, EmailContent.count(mProviderContext, Message.DELETED_CONTENT_URI,
                WHERE_ACCOUNT_KEY, getAccountArgument(mAccount.mId)));

        // Call code to send deletions; the id's of the ones actually deleted will be in the
        // deletedIds list
        mSyncAdapter.sendDeletedItems(s, deletedIds, true);
        assertEquals(3, deletedIds.size());

        // Clear this out for the next test
        deletedIds.clear();

        // Create a new message
        Message msg4 = EmailContentSetupUtils.setupMessage("message4", mAccount.mId, box1.mId,
                true, true, mProviderContext);
        assertEquals(1, EmailContent.count(mProviderContext, Message.CONTENT_URI, WHERE_ACCOUNT_KEY,
                getAccountArgument(mAccount.mId)));
        // Find the body for this message
        Body body = Body.restoreBodyWithMessageId(mProviderContext, msg4.mId);
        // Set its source message to msg2's id
        ContentValues values = new ContentValues();
        values.put(Body.SOURCE_MESSAGE_KEY, msg2.mId);
        body.update(mProviderContext, values);

        // Now send deletions again; this time only two should get deleted; msg2 should NOT be
        // deleted as it's referenced by msg4
        mSyncAdapter.sendDeletedItems(s, deletedIds, true);
        assertEquals(2, deletedIds.size());
        assertFalse(deletedIds.contains(msg2.mId));
    }

    private String[] getAccountArgument(long id) {
        ACCOUNT_ARGUMENT[0] = Long.toString(id);
        return ACCOUNT_ARGUMENT;
    }

    void setupSyncParserAndAdapter(Account account, Mailbox mailbox) throws IOException {
        EasSyncService service = getTestService(account, mailbox);
        mSyncAdapter = new EmailSyncAdapter(service);
        mSyncParser = mSyncAdapter.new EasEmailSyncParser(getTestInputStream(), mSyncAdapter);
    }

    ArrayList<Long> setupAccountMailboxAndMessages(int numMessages) {
        ArrayList<Long> ids = new ArrayList<Long>();

        // Create account and two mailboxes
        mAccount = EmailContentSetupUtils.setupAccount("account", true, mProviderContext);
        mMailbox = EmailContentSetupUtils.setupMailbox("box1", mAccount.mId, true,
                mProviderContext);

        for (int i = 0; i < numMessages; i++) {
            Message msg = EmailContentSetupUtils.setupMessage("message" + i, mAccount.mId,
                    mMailbox.mId, true, true, mProviderContext);
            ids.add(msg.mId);
        }

        assertEquals(numMessages, EmailContent.count(mProviderContext, Message.CONTENT_URI,
                WHERE_ACCOUNT_KEY, getAccountArgument(mAccount.mId)));
        return ids;
    }

    public void testDeleteParser() throws IOException {
        // Setup some messages
        ArrayList<Long> messageIds = setupAccountMailboxAndMessages(3);
        ContentValues cv = new ContentValues();
        cv.put(SyncColumns.SERVER_ID, TEST_SERVER_ID);
        long deleteMessageId = messageIds.get(1);
        mResolver.update(ContentUris.withAppendedId(Message.CONTENT_URI, deleteMessageId), cv,
                null, null);

        // Setup our adapter and parser
        setupSyncParserAndAdapter(mAccount, mMailbox);

        // Set up an input stream with a delete command
        Serializer s = new Serializer(false);
        s.start(Tags.SYNC_DELETE).data(Tags.SYNC_SERVER_ID, TEST_SERVER_ID).end().done();
        byte[] bytes = s.toByteArray();
        mSyncParser.resetInput(new ByteArrayInputStream(bytes));
        mSyncParser.nextTag(0);

        // Run the delete parser
        ArrayList<Long> deleteList = new ArrayList<Long>();
        mSyncParser.deleteParser(deleteList, Tags.SYNC_DELETE);
        // It should have found the message
        assertEquals(1, deleteList.size());
        long id = deleteList.get(0);
        // And the id's should match
        assertEquals(deleteMessageId, id);
    }

    /**
     * M: Test the CommandsParser
     */
    public void testCommandsParser() throws IOException, CommandStatusException {
        long expected = 34;
        ArrayList<Long> messageIds = setupAccountMailboxAndMessages(0);
        // Setup our adapter and parser
        setupSyncParserAndAdapter(mAccount, mMailbox);

        // Set up an input stream with a command
        Serializer s = new Serializer(false);
        s.start(Tags.SYNC_COMMANDS).start(Tags.SYNC_ADD).data(Tags.SYNC_SERVER_ID, TEST_SERVER_ID)
        .tag(Tags.SYNC_WAIT).data(Tags.SYNC_STATUS, "1").start(Tags.SYNC_APPLICATION_DATA)
        .tag(Tags.EMAIL_TO).tag(Tags.EMAIL_FROM).tag(Tags.EMAIL_CC).tag(Tags.EMAIL_REPLY_TO)
        .tag(Tags.EMAIL_SUBJECT).tag(Tags.EMAIL_BODY).tag(Tags.EMAIL_THREAD_TOPIC)
        .tag(Tags.RIGHTS_LICENSE).tag(Tags.EMAIL2_CONVERSATION_INDEX)
        .data(Tags.EMAIL_MIME_TRUNCATED, "1").data(Tags.EMAIL_MIME_DATA, "1")
        .data(Tags.EMAIL2_LAST_VERB_EXECUTED, "3").data(Tags.EMAIL2_LAST_VERB_EXECUTED, "2")
        .data(Tags.EMAIL_READ, "1").start(Tags.BASE_BODY).tag(Tags.BASE_DATA).tag(Tags.BASE_TYPE)
        .tag(Tags.BASE_TRUNCATED).tag(Tags.EMAIL).end().start(Tags.EMAIL_FLAG)
        .data(Tags.EMAIL_FLAG_STATUS, "2").tag(Tags.EMAIL).end().end().end().tag(Tags.SYNC_WAIT)
        .end().done();

        byte[] bytes = s.toByteArray();
        mSyncParser.resetInput(new ByteArrayInputStream(bytes));
        mSyncParser.nextTag(0);

        // Run the add parser
        mSyncParser.commandsParser();
        long resultSize = mSyncAdapter.mSize;
        assertEquals(expected, resultSize);
    }

    /**
     * M: Test the MeetingRequestParser
     */
    public void testMeetingRequestParser() throws IOException {
        ArrayList<Long> messageIds = setupAccountMailboxAndMessages(0);
        // Setup our adapter and parser
        setupSyncParserAndAdapter(mAccount, mMailbox);
        Message msg = new Message();
        String expected = "6" + '\2' + "UID" + '\1' + "1" + '\2' + "DTSTAMP"
                + '\1' + "7" + '\2' + "RESPONSE" + '\1' + "4" + '\2' + "ORGMAIL"
                + '\1' + "2012-01-02T23:00:01.000Z" + '\2' + "DTSTART"
                + '\1' + "1" + '\2' + "ALLDAY" + '\1' + "3" + '\2' + "DTEND"
                + '\1' + "8" + '\2' + "TITLE" + '\1' + "5" + '\2' + "LOC";
        msg.mSubject = "8";

        // Set up an input stream
        Serializer s = new Serializer(false);
        s.start(Tags.SYNC_APPLICATION_DATA).start(Tags.EMAIL_MEETING_REQUEST)
        .data(Tags.EMAIL_DTSTAMP, "1").data(Tags.EMAIL_START_TIME, "2012-01-02T23:00:01.000Z")
        .data(Tags.EMAIL_END_TIME, "3").data(Tags.EMAIL_ORGANIZER, "4")
        .data(Tags.EMAIL_LOCATION, "5").data(Tags.EMAIL_GLOBAL_OBJID, "6")
        .tag(Tags.EMAIL_CATEGORIES).data(Tags.EMAIL_RESPONSE_REQUESTED, "7")
        .data(Tags.EMAIL_ALL_DAY_EVENT, "1").start(Tags.EMAIL_RECURRENCES)
        .tag(Tags.EMAIL_RECURRENCE).tag(Tags.EMAIL).end().tag(Tags.EMAIL)
        .end().data(Tags.EMAIL_MESSAGE_CLASS, "IPM.Schedule.Meeting.Request")
        .data(Tags.EMAIL_BODY, "")
        .end().done();

        byte[] bytes = s.toByteArray();
        mSyncParser.resetInput(new ByteArrayInputStream(bytes));
        mSyncParser.nextTag(0);

        mSyncParser.addData(msg, Tags.SYNC_APPLICATION_DATA);
        // Make sure the mMeetingInfo
        assertEquals(expected, msg.mMeetingInfo);
    }

    /**
     * M: Test the AddData function
     */
    public void testAddData() throws IOException {
        Message msg = new Message();
        ArrayList<Long> messageIds = setupAccountMailboxAndMessages(0);
        // Setup our adapter and parser
        setupSyncParserAndAdapter(mAccount, mMailbox);

        // Set up an input stream
        Serializer s = new Serializer(false);
        s.start(Tags.SYNC_APPLICATION_DATA).start(Tags.EMAIL_ATTACHMENTS)
        .start(Tags.EMAIL_ATTACHMENT).data(Tags.EMAIL_DISPLAY_NAME, "123.jpg")
        .data(Tags.EMAIL_ATT_NAME, "1").data(Tags.EMAIL_ATT_SIZE, "50")
        .data(Tags.BASE_IS_INLINE, "true").data(Tags.BASE_CONTENT_ID, "1")
        .end().end().end().done();

        byte[] bytes = s.toByteArray();
        mSyncParser.resetInput(new ByteArrayInputStream(bytes));
        mSyncParser.nextTag(0);

        mSyncParser.addData(msg, Tags.SYNC_APPLICATION_DATA);
        // Make sure the mMeetingInfo
        Attachment att = msg.mAttachments.get(0);
        assertEquals("123.jpg", att.mFileName);
        assertEquals("1", att.mLocation);
        assertEquals("image/jpeg", att.mMimeType);
        assertEquals("1", att.mContentId);
        assertEquals(Long.parseLong("50"), att.mSize);
        assertEquals(false, msg.mFlagAttachment);
    }

    public void testSendSyncOptions() throws IOException {
        Double protocolVersion = 2.5;
        int syncStatus = Eas.EAS_SYNC_NORMAL;
        Serializer s1 = new Serializer();
        s1.start(Tags.SYNC_SYNC)
        .start(Tags.SYNC_COLLECTIONS)
        .start(Tags.SYNC_COLLECTION);
        EmailSyncAdapter adapter = new EmailSyncAdapter(getTestService());
        adapter.mMailbox.mType = Mailbox.TYPE_INBOX;
        adapter.mAccount.mSyncLookback = 5;
        adapter.sendSyncOptions(protocolVersion, s1, syncStatus);
        Serializer s2 = new Serializer();
        s2.start(Tags.SYNC_SYNC)
        .start(Tags.SYNC_COLLECTIONS)
        .start(Tags.SYNC_COLLECTION)
        .tag(Tags.SYNC_DELETES_AS_MOVES)
        .tag(Tags.SYNC_GET_CHANGES)
        .data(Tags.SYNC_WINDOW_SIZE, "10")
        .start(Tags.SYNC_OPTIONS)
        .data(Tags.SYNC_FILTER_TYPE, Eas.FILTER_1_MONTH)
        .data(Tags.SYNC_MIME_SUPPORT, Eas.MIME_BODY_PREFERENCE_MIME)
        .data(Tags.SYNC_MIME_TRUNCATION, Eas.EAS2_5_FULL_SIZE)
        .end();
        assertEquals(s2.toString(), s1.toString());
    }

    public void testGetWindowCount() throws IOException {
        int expected1 = 2;
        ArrayList<Long> ids = new ArrayList<Long>();

        // Create account and two mailboxes
        mAccount = EmailContentSetupUtils.setupEasAccount("account", true, mProviderContext);
        mMailbox = EmailContentSetupUtils.setupMailbox("box1", mAccount.mId, true,
                mProviderContext);

        for (int i = 0; i < 3; i++) {
            Message msg = EmailContentSetupUtils.setupMessage("message" + i, mAccount.mId,
                    mMailbox.mId, true, true, mProviderContext);
            ids.add(msg.mId);
        }
        // Setup our adapter and parser
        setupSyncParserAndAdapter(mAccount, mMailbox);
        Configuration.openTest();
        EasResponse.sCallback = new MockEasResponse();
        int result;
        try {
            long msgId = ids.get(1);
            Message msg = Message.restoreMessageWithId(mProviderContext, msgId);
            Account account = Account.restoreAccountWithId(mProviderContext,
                    msg.mAccountKey);
            account.mProtocolVersion = "14.1";
            mSyncAdapter.mService = EasSyncService.setupServiceForAccount(
                    mProviderContext, account);
            mSyncAdapter.mService.mProtocolVersionDouble = 2.5;
            result = mSyncAdapter.getWindowCount();
            assertEquals(expected1, result);
            mSyncAdapter.mService = EasSyncService.setupServiceForAccount(
                    mProviderContext, account);
            mSyncAdapter.mService.mProtocolVersionDouble = 12.0;
            result = mSyncAdapter.getWindowCount();
            assertEquals(expected1, result);
            mSyncAdapter.mService = EasSyncService.setupServiceForAccount(
                    mProviderContext, account);
            mSyncAdapter.mService.mProtocolVersionDouble = 14.0;
            result = mSyncAdapter.getWindowCount();
        } finally {
            Configuration.shutDownTest();
            EasResponse.sCallback = null;
        }
        assertEquals(expected1, result);
    }

    public void testChangeParser() throws IOException {
        // Setup some messages
        ArrayList<Long> messageIds = setupAccountMailboxAndMessages(3);
        ContentValues cv = new ContentValues();
        int randomFlags = Message.FLAG_INCOMING_MEETING_CANCEL | Message.FLAG_TYPE_FORWARD;
        cv.put(SyncColumns.SERVER_ID, TEST_SERVER_ID);
        cv.put(MessageColumns.FLAGS, randomFlags);
        long changeMessageId = messageIds.get(1);
        mResolver.update(ContentUris.withAppendedId(Message.CONTENT_URI, changeMessageId), cv,
                null, null);

        // Setup our adapter and parser
        setupSyncParserAndAdapter(mAccount, mMailbox);

        // Set up an input stream with a change command (marking TEST_SERVER_ID unread)
        // Note that the test message creation code sets read to "true"
        Serializer s = new Serializer(false);
        s.start(Tags.SYNC_CHANGE).data(Tags.SYNC_SERVER_ID, TEST_SERVER_ID);
        s.start(Tags.SYNC_APPLICATION_DATA);
        s.data(Tags.EMAIL_READ, "0");
        s.data(Tags.EMAIL2_LAST_VERB_EXECUTED,
                Integer.toString(EmailSyncAdapter.LAST_VERB_FORWARD));
        s.end().end().done();
        byte[] bytes = s.toByteArray();
        mSyncParser.resetInput(new ByteArrayInputStream(bytes));
        mSyncParser.nextTag(0);

        // Run the delete parser
        ArrayList<ServerChange> changeList = new ArrayList<ServerChange>();
        mSyncParser.changeParser(changeList);
        // It should have found the message
        assertEquals(1, changeList.size());
        // And the id's should match
        ServerChange change = changeList.get(0);
        assertEquals(changeMessageId, change.id);
        assertNotNull(change.read);
        assertFalse(change.read);
        // Make sure we see the forwarded flag AND that the original flags are preserved
        assertEquals((Integer)(randomFlags | Message.FLAG_FORWARDED), change.flags);
    }

    /**
     * M: Test the Parser
     */
    public void testParse() throws IOException, CommandStatusException, NumberFormatException {
        boolean result;
        // Setup some messages
        ArrayList<Long> messageIds = setupAccountMailboxAndMessages(3);
        ContentValues cv = new ContentValues();
        int randomFlags = Message.FLAG_INCOMING_MEETING_CANCEL | Message.FLAG_TYPE_FORWARD;
        cv.put(SyncColumns.SERVER_ID, TEST_SERVER_ID);
        cv.put(MessageColumns.FLAGS, randomFlags);
        long changeMessageId = messageIds.get(1);
        mResolver.update(ContentUris.withAppendedId(Message.CONTENT_URI, changeMessageId), cv,
                null, null);

        // Setup our adapter and parser
        setupSyncParserAndAdapter(mAccount, mMailbox);

        Serializer s = new Serializer();
        s.start(Tags.SYNC_SYNC).tag(Tags.SYNC_COLLECTIONS).start(Tags.SYNC_COMMANDS)
        .tag(Tags.SYNC_CHANGE).end().start(Tags.SYNC_RESPONSES).start(Tags.SYNC_DELETE)
        .tag(Tags.SYNC_VERSION).data(Tags.SYNC_SERVER_ID, TEST_SERVER_ID)
        .data(Tags.SYNC_STATUS, "7").end().start(Tags.SYNC_FETCH).tag(Tags.SYNC_VERSION)
        .data(Tags.SYNC_SERVER_ID, TEST_SERVER_ID).start(Tags.SYNC_APPLICATION_DATA)
        .data(Tags.EMAIL_BODY, "Email").end().end().end()
        .tag(Tags.SYNC_MORE_AVAILABLE).data(Tags.SYNC_STATUS, "3").data(Tags.SYNC_STATUS, "7")
        .data(Tags.SYNC_SYNC_KEY, "sync-key-box").tag(Tags.SYNC_VERSION).end().done();

        byte[] bytes = s.toByteArray();
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        result = mSyncAdapter.parse(bis);
        assertTrue(result);
    }

    public void testSendLocalChanges() throws IOException {
        // Setup some messages
        ArrayList<Long> ids = new ArrayList<Long>();

        // Create account and two mailboxes
        mAccount = EmailContentSetupUtils.setupAccount("account", true, mProviderContext);
        mMailbox = EmailContentSetupUtils.setupMailbox("box1", mAccount.mId, true,
                mProviderContext);
        Message msg = EmailContentSetupUtils.setupMessage("message" + 0, mAccount.mId,
                mMailbox.mId, true, true, mProviderContext, false, true);
        ids.add(msg.mId);
        // Setup our adapter and parser
        setupSyncParserAndAdapter(mAccount, mMailbox);

        // Change one message
        long id = ids.get(0);
        ContentValues cv = new ContentValues();
        cv.put(Message.FLAG_READ, 0);
        cv.put(SyncColumns.SERVER_ID, "TEST_SERVER_ID");
        cv.put(MessageColumns.MAILBOX_KEY, 6);
        cv.put(MessageColumns.FLAG_FAVORITE, true);
        mResolver.update(ContentUris.withAppendedId(Message.SYNCED_CONTENT_URI,
                id), cv, null, null);
        mSyncAdapter.mService.mProtocolVersionDouble = 14.0;

        Serializer s = new Serializer();
        mSyncAdapter.sendLocalChanges(s);
        String tString = s.toString();
        assertNotNull(tString);
    }

    public void testCleanup() throws IOException {
        // Setup some messages
        ArrayList<Long> messageIds = setupAccountMailboxAndMessages(3);
        // Setup our adapter and parser
        setupSyncParserAndAdapter(mAccount, mMailbox);

        // Delete two of the messages, change one
        long id = messageIds.get(0);
        mResolver.delete(ContentUris.withAppendedId(Message.SYNCED_CONTENT_URI, id),
                null, null);
        mSyncAdapter.mDeletedIdList.add(id);
        id = messageIds.get(1);
        mResolver.delete(ContentUris.withAppendedId(Message.SYNCED_CONTENT_URI,
                id), null, null);
        mSyncAdapter.mDeletedIdList.add(id);
        id = messageIds.get(2);
        ContentValues cv = new ContentValues();
        cv.put(Message.FLAG_READ, 0);
        mResolver.update(ContentUris.withAppendedId(Message.SYNCED_CONTENT_URI,
                id), cv, null, null);
        mSyncAdapter.mUpdatedIdList.add(id);

        // The changed message should still exist
        assertEquals(1, EmailContent.count(mProviderContext, Message.CONTENT_URI, WHERE_ACCOUNT_KEY,
                getAccountArgument(mAccount.mId)));

        // As well, the two deletions and one update
        assertEquals(2, EmailContent.count(mProviderContext, Message.DELETED_CONTENT_URI,
                WHERE_ACCOUNT_KEY, getAccountArgument(mAccount.mId)));
        assertEquals(1, EmailContent.count(mProviderContext, Message.UPDATED_CONTENT_URI,
                WHERE_ACCOUNT_KEY, getAccountArgument(mAccount.mId)));

        // Cleanup (i.e. after sync); should remove items from delete/update tables
        mSyncAdapter.cleanup();

        // The three should be gone
        assertEquals(0, EmailContent.count(mProviderContext, Message.DELETED_CONTENT_URI,
                WHERE_ACCOUNT_KEY, getAccountArgument(mAccount.mId)));
        assertEquals(0, EmailContent.count(mProviderContext, Message.UPDATED_CONTENT_URI,
                WHERE_ACCOUNT_KEY, getAccountArgument(mAccount.mId)));
    }
}
