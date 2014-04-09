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

import com.android.emailcommon.Configuration;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.provider.EmailContent.Message;
import com.android.exchange.CommandStatusException;
import com.android.exchange.Eas;
import com.android.exchange.EasSyncService;
import com.android.exchange.adapter.CalendarSyncAdapter;
import com.android.exchange.adapter.EmailSyncAdapter;
import com.android.exchange.adapter.Serializer;
import com.android.exchange.adapter.Tags;
import com.android.exchange.adapter.AbstractSyncAdapter.Operation;
import com.android.exchange.adapter.CalendarSyncAdapter.CalendarOperations;
import com.android.exchange.adapter.CalendarSyncAdapter.EasCalendarSyncParser;
import com.android.exchange.adapter.EmailSyncAdapter.EasEmailSyncParser;
import com.android.exchange.provider.EmailContentSetupUtils;
import com.android.exchange.provider.MockProvider;

import android.accounts.AccountManager;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Entity;
import android.content.EntityIterator;
import android.content.OperationApplicationException;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.RemoteException;
import android.provider.CalendarContract;
import android.provider.SyncStateContract;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.EventsEntity;
import android.provider.CalendarContract.ExtendedProperties;
import android.provider.CalendarContract.SyncState;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.test.IsolatedContext;
import android.test.RenamingDelegatingContext;
import android.test.mock.MockContentResolver;
import android.test.mock.MockContext;
import android.test.suitebuilder.annotation.MediumTest;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

/**
 * You can run this entire test case with:
 *   runtest -c com.android.exchange.adapter.CalendarSyncAdapterTests exchange
 */
@MediumTest
public class CalendarSyncAdapterTests extends SyncAdapterTestCase<CalendarSyncAdapter> {
    private static final String[] ATTENDEE_PROJECTION = new String[] {Attendees.ATTENDEE_EMAIL,
            Attendees.ATTENDEE_NAME, Attendees.ATTENDEE_STATUS};
    private static final int ATTENDEE_EMAIL = 0;
    private static final int ATTENDEE_NAME = 1;
    private static final int ATTENDEE_STATUS = 2;

    private static final String SINGLE_ATTENDEE_EMAIL = "attendee@host.com";
    private static final String SINGLE_ATTENDEE_NAME = "Bill Attendee";
    private static final String WHERE_ACCOUNT_KEY = Message.ACCOUNT_KEY + "=?";
    private static final String EVENT_SYNC_MARK = Events.SYNC_DATA8;
    private static final String DIRTY_OR_MARKED_TOP_LEVEL_IN_CALENDAR = "(" + Events.DIRTY
            + "=1 OR " + EVENT_SYNC_MARK + "= 1) AND " +
            Events.ORIGINAL_ID + " ISNULL AND " + Events.CALENDAR_ID + "=?";
    private static final String[] ACCOUNT_ARGUMENT = new String[1];
    protected static final String TEST_ACCOUNT_SUFFIX = "@android.com";

    private Context mMockContext;
    private MockContentResolver mMockResolver;
    private CalendarSyncAdapter mAdapter;

    // This is the US/Pacific time zone as a base64-encoded TIME_ZONE_INFORMATION structure, as
    // it would appear coming from an Exchange server
    private static final String TEST_TIME_ZONE = "4AEAAFAAYQBjAGkAZgBpAGMAIABTAHQAYQBuAGQAYQByA" +
        "GQAIABUAGkAbQBlAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAsAAAABAAIAAAAAAAAAAAAAAFAAY" +
        "QBjAGkAZgBpAGMAIABEAGEAeQBsAGkAZwBoAHQAIABUAGkAbQBlAAAAAAAAAAAAAAAAAAAAAAAAA" +
        "AAAAAAAAAMAAAACAAIAAAAAAAAAxP///w==";

    private class MockContext2 extends MockContext {

        @Override
        public Resources getResources() {
            return getContext().getResources();
        }

        @Override
        public File getDir(String name, int mode) {
            // name the directory so the directory will be separated from
            // one created through the regular Context
            return getContext().getDir("mockcontext2_" + name, mode);
        }

        @Override
        public Context getApplicationContext() {
            return this;
        }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mMockResolver = new MockContentResolver();
        final String filenamePrefix = "test.";
        RenamingDelegatingContext targetContextWrapper = new
        RenamingDelegatingContext(
                new MockContext2(), // The context that most methods are delegated to
                getContext(), // The context that file methods are delegated to
                filenamePrefix);
        mMockContext = new IsolatedContext(mMockResolver, targetContextWrapper);
        mMockResolver.addProvider(MockProvider.AUTHORITY, new MockProvider(mMockContext));
    }

    public CalendarSyncAdapterTests() {
        super();
    }

    public void testSetTimeRelatedValues_NonRecurring() throws IOException {
        CalendarSyncAdapter adapter = getTestSyncAdapter(CalendarSyncAdapter.class);
        EasCalendarSyncParser p = adapter.new EasCalendarSyncParser(getTestInputStream(), adapter);
        ContentValues cv = new ContentValues();
        // Basic, one-time meeting lasting an hour
        GregorianCalendar startCalendar = new GregorianCalendar(2010, 5, 10, 8, 30);
        Long startTime = startCalendar.getTimeInMillis();
        GregorianCalendar endCalendar = new GregorianCalendar(2010, 5, 10, 9, 30);
        Long endTime = endCalendar.getTimeInMillis();

        p.setTimeRelatedValues(cv, startTime, endTime, 0);
        assertNull(cv.getAsInteger(Events.DURATION));
        assertEquals(startTime, cv.getAsLong(Events.DTSTART));
        assertEquals(endTime, cv.getAsLong(Events.DTEND));
        assertEquals(endTime, cv.getAsLong(Events.LAST_DATE));
        assertNull(cv.getAsString(Events.EVENT_TIMEZONE));
    }

    public void testSetTimeRelatedValues_Recurring() throws IOException {
        CalendarSyncAdapter adapter = getTestSyncAdapter(CalendarSyncAdapter.class);
        EasCalendarSyncParser p = adapter.new EasCalendarSyncParser(getTestInputStream(), adapter);
        ContentValues cv = new ContentValues();
        // Recurring meeting lasting an hour
        GregorianCalendar startCalendar = new GregorianCalendar(2010, 5, 10, 8, 30);
        Long startTime = startCalendar.getTimeInMillis();
        GregorianCalendar endCalendar = new GregorianCalendar(2010, 5, 10, 9, 30);
        Long endTime = endCalendar.getTimeInMillis();
        cv.put(Events.RRULE, "FREQ=DAILY");
        p.setTimeRelatedValues(cv, startTime, endTime, 0);
        assertEquals("P60M", cv.getAsString(Events.DURATION));
        assertEquals(startTime, cv.getAsLong(Events.DTSTART));
        assertNull(cv.getAsLong(Events.DTEND));
        assertNull(cv.getAsLong(Events.LAST_DATE));
        assertNull(cv.getAsString(Events.EVENT_TIMEZONE));
    }

    public void testSetTimeRelatedValues_AllDay() throws IOException {
        CalendarSyncAdapter adapter = getTestSyncAdapter(CalendarSyncAdapter.class);
        EasCalendarSyncParser p = adapter.new EasCalendarSyncParser(getTestInputStream(), adapter);
        ContentValues cv = new ContentValues();
        GregorianCalendar startCalendar = new GregorianCalendar(2010, 5, 10, 8, 30);
        Long startTime = startCalendar.getTimeInMillis();
        GregorianCalendar endCalendar = new GregorianCalendar(2010, 5, 11, 8, 30);
        Long endTime = endCalendar.getTimeInMillis();
        cv.put(Events.RRULE, "FREQ=WEEKLY;BYDAY=MO");
        p.setTimeRelatedValues(cv, startTime, endTime, 1);

        // The start time should have hour/min/sec zero'd out
        startCalendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        startCalendar.set(2010, 5, 10, 0, 0, 0);
        startCalendar.set(GregorianCalendar.MILLISECOND, 0);
        startTime = startCalendar.getTimeInMillis();
        assertEquals(startTime, cv.getAsLong(Events.DTSTART));

        // The duration should be in days
        assertEquals("P1D", cv.getAsString(Events.DURATION));
        assertNull(cv.getAsLong(Events.DTEND));
        assertNull(cv.getAsLong(Events.LAST_DATE));
        // There must be a timezone
        assertNotNull(cv.getAsString(Events.EVENT_TIMEZONE));
    }

    public void testSetTimeRelatedValues_Recurring_AllDay_Exception () throws IOException {
        CalendarSyncAdapter adapter = getTestSyncAdapter(CalendarSyncAdapter.class);
        EasCalendarSyncParser p = adapter.new EasCalendarSyncParser(getTestInputStream(), adapter);
        ContentValues cv = new ContentValues();

        // Recurrence exception for all-day event; the exception is NOT all-day
        GregorianCalendar startCalendar = new GregorianCalendar(2010, 5, 17, 8, 30);
        Long startTime = startCalendar.getTimeInMillis();
        GregorianCalendar endCalendar = new GregorianCalendar(2010, 5, 17, 9, 30);
        Long endTime = endCalendar.getTimeInMillis();
        cv.put(Events.ORIGINAL_ALL_DAY, 1);
        GregorianCalendar instanceCalendar = new GregorianCalendar(2010, 5, 17, 8, 30);
        cv.put(Events.ORIGINAL_INSTANCE_TIME, instanceCalendar.getTimeInMillis());
        p.setTimeRelatedValues(cv, startTime, endTime, 0);

        // The original instance time should have hour/min/sec zero'd out
        GregorianCalendar testCalendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        testCalendar.set(2010, 5, 17, 0, 0, 0);
        testCalendar.set(GregorianCalendar.MILLISECOND, 0);
        Long testTime = testCalendar.getTimeInMillis();
        assertEquals(testTime, cv.getAsLong(Events.ORIGINAL_INSTANCE_TIME));

        // The exception isn't all-day, so we should have DTEND and LAST_DATE and no EVENT_TIMEZONE
        assertNull(cv.getAsString(Events.DURATION));
        assertEquals(endTime, cv.getAsLong(Events.DTEND));
        assertEquals(endTime, cv.getAsLong(Events.LAST_DATE));
        assertNull(cv.getAsString(Events.EVENT_TIMEZONE));
    }

    public void testIsValidEventValues() throws IOException {
        CalendarSyncAdapter adapter = getTestSyncAdapter(CalendarSyncAdapter.class);
        EasCalendarSyncParser p = adapter.new EasCalendarSyncParser(getTestInputStream(), adapter);

        long validTime = System.currentTimeMillis();
        String validData = "foo-bar-bletch";
        String validDuration = "P30M";
        String validRrule = "FREQ=DAILY";

        ContentValues cv = new ContentValues();

        cv.put(Events.DTSTART, validTime);
        // Needs _SYNC_DATA and DTEND/DURATION
        assertFalse(p.isValidEventValues(cv));
        cv.put(Events.SYNC_DATA2, validData);
        // Needs DTEND/DURATION since not an exception
        assertFalse(p.isValidEventValues(cv));
        cv.put(Events.DURATION, validDuration);
        // Valid (DTSTART, _SYNC_DATA, DURATION)
        assertTrue(p.isValidEventValues(cv));
        cv.remove(Events.DURATION);
        cv.put(Events.ORIGINAL_INSTANCE_TIME, validTime);
        // Needs DTEND since it's an exception
        assertFalse(p.isValidEventValues(cv));
        cv.put(Events.DTEND, validTime);
        // Valid (DTSTART, DTEND, ORIGINAL_INSTANCE_TIME)
        cv.remove(Events.ORIGINAL_INSTANCE_TIME);
        // Valid (DTSTART, _SYNC_DATA, DTEND)
        assertTrue(p.isValidEventValues(cv));
        cv.remove(Events.DTSTART);
        // Needs DTSTART
        assertFalse(p.isValidEventValues(cv));
        cv.put(Events.DTSTART, validTime);
        cv.put(Events.RRULE, validRrule);
        // With RRULE, needs DURATION
        assertFalse(p.isValidEventValues(cv));
        cv.put(Events.DURATION, "P30M");
        // Valid (DTSTART, RRULE, DURATION)
        assertTrue(p.isValidEventValues(cv));
        cv.put(Events.ALL_DAY, "1");
        // Needs DURATION in the form P<n>D
        assertFalse(p.isValidEventValues(cv));
        // Valid (DTSTART, RRULE, ALL_DAY, DURATION(P<n>D)
        cv.put(Events.DURATION, "P1D");
        assertTrue(p.isValidEventValues(cv));
    }

    private void addAttendeesToSerializer(Serializer s, int num) throws IOException {
        for (int i = 0; i < num; i++) {
            s.start(Tags.CALENDAR_ATTENDEE);
            s.data(Tags.CALENDAR_ATTENDEE_EMAIL, "frederick" + num +
                    ".flintstone@this.that.verylongservername.com");
            s.data(Tags.CALENDAR_ATTENDEE_TYPE, "1");
            s.data(Tags.CALENDAR_ATTENDEE_NAME, "Frederick" + num + " Flintstone, III");
            s.end();
        }
    }

    private void addAttendeeToSerializer(Serializer s, String email, String name)
            throws IOException {
        s.start(Tags.CALENDAR_ATTENDEE);
        s.data(Tags.CALENDAR_ATTENDEE_EMAIL, email);
        s.data(Tags.CALENDAR_ATTENDEE_TYPE, "1");
        s.data(Tags.CALENDAR_ATTENDEE_NAME, name);
        s.end();
    }

    private int countInsertOperationsForTable(CalendarOperations ops, String tableName) {
        int cnt = 0;
        for (Operation op: ops) {
            ContentProviderOperation cpo =
                    AbstractSyncAdapter.operationToContentProviderOperation(op, 0);
            List<String> segments = cpo.getUri().getPathSegments();
            if (segments.get(0).equalsIgnoreCase(tableName) &&
                    cpo.getType() == ContentProviderOperation.TYPE_INSERT) {
                cnt++;
            }
        }
        return cnt;
    }

    class TestEvent extends Serializer {
        CalendarSyncAdapter mAdapter;
        EasCalendarSyncParser mParser;
        Serializer mSerializer;

        TestEvent() throws IOException {
            super(false);
            mAdapter = getTestSyncAdapter(CalendarSyncAdapter.class);
            mParser = mAdapter.new EasCalendarSyncParser(getTestInputStream(), mAdapter);
        }

        void setUserEmailAddress(String addr) {
            mAdapter.mAccount.mEmailAddress = addr;
            mAdapter.mEmailAddress = addr;
        }

        EasCalendarSyncParser getParser() throws IOException {
            // Set up our parser's input and eat the initial tag
            mParser.resetInput(new ByteArrayInputStream(toByteArray()));
            mParser.nextTag(0);
            return mParser;
        }

        // setupPreAttendees and setupPostAttendees initialize calendar data in the order in which
        // they would appear in an actual EAS session.  Between these two calls, we initialize
        // attendee data, which varies between the following tests
        TestEvent setupPreAttendees() throws IOException {
            start(Tags.SYNC_APPLICATION_DATA);
            data(Tags.CALENDAR_TIME_ZONE, TEST_TIME_ZONE);
            data(Tags.CALENDAR_DTSTAMP, "20100518T213156Z");
            data(Tags.CALENDAR_START_TIME, "20100518T220000Z");
            data(Tags.CALENDAR_SUBJECT, "Documentation");
            data(Tags.CALENDAR_UID, "4417556B-27DE-4ECE-B679-A63EFE1F9E85");
            data(Tags.CALENDAR_ORGANIZER_NAME, "Fred Squatibuquitas");
            data(Tags.CALENDAR_ORGANIZER_EMAIL, "fred.squatibuquitas@prettylongdomainname.com");
            return this;
        }

        TestEvent setupPostAttendees()throws IOException {
            data(Tags.CALENDAR_LOCATION, "CR SF 601T2/North Shore Presentation Self Service (16)");
            data(Tags.CALENDAR_END_TIME, "20100518T223000Z");
            start(Tags.BASE_BODY);
            data(Tags.BASE_BODY_PREFERENCE, "1");
            data(Tags.BASE_ESTIMATED_DATA_SIZE, "69105"); // The number is ignored by the parser
            data(Tags.BASE_DATA,
                    "This is the event description; we should probably make it longer");
            end(); // BASE_BODY
            start(Tags.CALENDAR_RECURRENCE);
            data(Tags.CALENDAR_RECURRENCE_TYPE, "1"); // weekly
            data(Tags.CALENDAR_RECURRENCE_INTERVAL, "1");
            data(Tags.CALENDAR_RECURRENCE_OCCURRENCES, "10");
            data(Tags.CALENDAR_RECURRENCE_DAYOFWEEK, "12"); // tue, wed
            data(Tags.CALENDAR_RECURRENCE_UNTIL, "2005-04-14T00:00:00.000Z");
            end();  // CALENDAR_RECURRENCE
            data(Tags.CALENDAR_SENSITIVITY, "0");
            data(Tags.CALENDAR_BUSY_STATUS, "2");
            data(Tags.CALENDAR_ALL_DAY_EVENT, "0");
            data(Tags.CALENDAR_MEETING_STATUS, "3");
            data(Tags.BASE_NATIVE_BODY_TYPE, "3");
            end().done(); // SYNC_APPLICATION_DATA
            return this;
        }
    }

    public void testAddEvent() throws IOException {
        TestEvent event = new TestEvent();
        event.setupPreAttendees();
        event.start(Tags.CALENDAR_ATTENDEES);
        addAttendeesToSerializer(event, 10);
        event.end(); // CALENDAR_ATTENDEES
        event.setupPostAttendees();

        EasCalendarSyncParser p = event.getParser();
        p.addEvent(p.mOps, "1:1", false);
        // There should be 1 event
        assertEquals(1, countInsertOperationsForTable(p.mOps, "events"));
        // Two attendees (organizer and 10 attendees)
        assertEquals(11, countInsertOperationsForTable(p.mOps, "attendees"));
        // dtstamp, meeting status, attendees, attendees redacted, and upsync prohibited
        assertEquals(5, countInsertOperationsForTable(p.mOps, "extendedproperties"));
    }

    public void testAddEventIllegal() throws IOException {
        // We don't send a start time; the event is illegal and nothing should be added
        TestEvent event = new TestEvent();
        event.start(Tags.SYNC_APPLICATION_DATA);
        event.data(Tags.CALENDAR_TIME_ZONE, TEST_TIME_ZONE);
        event.data(Tags.CALENDAR_DTSTAMP, "20100518T213156Z");
        event.data(Tags.CALENDAR_SUBJECT, "Documentation");
        event.data(Tags.CALENDAR_UID, "4417556B-27DE-4ECE-B679-A63EFE1F9E85");
        event.data(Tags.CALENDAR_ORGANIZER_NAME, "Fred Squatibuquitas");
        event.data(Tags.CALENDAR_ORGANIZER_EMAIL, "fred.squatibuquitas@prettylongdomainname.com");
        event.start(Tags.CALENDAR_ATTENDEES);
        addAttendeesToSerializer(event, 10);
        event.end(); // CALENDAR_ATTENDEES
        event.setupPostAttendees();

        EasCalendarSyncParser p = event.getParser();
        p.addEvent(p.mOps, "1:1", false);
        assertEquals(0, countInsertOperationsForTable(p.mOps, "events"));
        assertEquals(0, countInsertOperationsForTable(p.mOps, "attendees"));
        assertEquals(0, countInsertOperationsForTable(p.mOps, "extendedproperties"));
    }

    public void testAddEventRedactedAttendees() throws IOException {
        TestEvent event = new TestEvent();
        event.setupPreAttendees();
        event.start(Tags.CALENDAR_ATTENDEES);
        addAttendeesToSerializer(event, 100);
        event.end(); // CALENDAR_ATTENDEES
        event.setupPostAttendees();

        EasCalendarSyncParser p = event.getParser();
        p.addEvent(p.mOps, "1:1", false);
        // There should be 1 event
        assertEquals(1, countInsertOperationsForTable(p.mOps, "events"));
        // One attendees (organizer; all others are redacted)
        assertEquals(1, countInsertOperationsForTable(p.mOps, "attendees"));
        // dtstamp, meeting status, and attendees redacted
        assertEquals(3, countInsertOperationsForTable(p.mOps, "extendedproperties"));
    }

    /**
     * Setup for the following three tests, which check attendee status of an added event
     * @param userEmail the email address of the user
     * @param update whether or not the event is an update (rather than new)
     * @return a Cursor to the Attendee records added to our MockProvider
     * @throws IOException
     * @throws RemoteException
     * @throws OperationApplicationException
     */
    private Cursor setupAddEventOneAttendee(String userEmail, boolean update)
            throws IOException, RemoteException, OperationApplicationException {
        TestEvent event = new TestEvent();
        event.setupPreAttendees();
        event.start(Tags.CALENDAR_ATTENDEES);
        addAttendeeToSerializer(event, SINGLE_ATTENDEE_EMAIL, SINGLE_ATTENDEE_NAME);
        event.setUserEmailAddress(userEmail);
        event.end(); // CALENDAR_ATTENDEES
        event.setupPostAttendees();

        EasCalendarSyncParser p = event.getParser();
        p.addEvent(p.mOps, "1:1", update);
        // Send the CPO's to the mock provider
        ArrayList<ContentProviderOperation> cpos = new ArrayList<ContentProviderOperation>();
        for (Operation op: p.mOps) {
            cpos.add(AbstractSyncAdapter.operationToContentProviderOperation(op, 0));
        }
        mMockResolver.applyBatch(MockProvider.AUTHORITY, cpos);
        return mMockResolver.query(MockProvider.uri(Attendees.CONTENT_URI), ATTENDEE_PROJECTION,
                null, null, null);
    }

    public void testAddEventOneAttendee() throws IOException, RemoteException,
            OperationApplicationException {
        Cursor c = setupAddEventOneAttendee("foo@bar.com", false);
        if (c != null) {
            try {
                assertEquals(2, c.getCount());
                // The organizer should be "accepted", the unknown attendee "none"
                while (c.moveToNext()) {
                    if (SINGLE_ATTENDEE_EMAIL.equals(c.getString(ATTENDEE_EMAIL))) {
                        assertEquals(Attendees.ATTENDEE_STATUS_NONE, c.getInt(ATTENDEE_STATUS));
                    } else {
                        assertEquals(Attendees.ATTENDEE_STATUS_ACCEPTED, c.getInt(ATTENDEE_STATUS));
                    }
                }
            } finally {
                c.close();
            }
        }
    }

    public void testAddEventSelfAttendee() throws IOException, RemoteException,
            OperationApplicationException {
        Cursor c = setupAddEventOneAttendee(SINGLE_ATTENDEE_EMAIL, false);
        // The organizer should be "accepted", and our user/attendee should be "done" even though
        // the busy status = 2 (because we can't tell from a status of 2 on new events)
        if (c != null) {
            try {
                while (c.moveToNext()) {
                    if (SINGLE_ATTENDEE_EMAIL.equals(c.getString(ATTENDEE_EMAIL))) {
                        assertEquals(Attendees.ATTENDEE_STATUS_ACCEPTED, c.getInt(ATTENDEE_STATUS));
                    } else {
                        assertEquals(Attendees.ATTENDEE_STATUS_ACCEPTED, c.getInt(ATTENDEE_STATUS));
                    }
                }
            } finally {
                c.close();
            }
        }
    }

    public void testAddEventSelfAttendeeUpdate() throws IOException, RemoteException,
            OperationApplicationException {
        Cursor c = setupAddEventOneAttendee(SINGLE_ATTENDEE_EMAIL, true);
        // The organizer should be "accepted", and our user/attendee should be "accepted" (because
        // busy status = 2 and this is an update
        if (c != null) {
            try {
                while (c.moveToNext()) {
                    if (SINGLE_ATTENDEE_EMAIL.equals(c.getString(ATTENDEE_EMAIL))) {
                        assertEquals(Attendees.ATTENDEE_STATUS_ACCEPTED, c.getInt(ATTENDEE_STATUS));
                    } else {
                        assertEquals(Attendees.ATTENDEE_STATUS_ACCEPTED, c.getInt(ATTENDEE_STATUS));
                    }
                }
            } finally {
                c.close();
            }
        }
    }

    public void testEasCalendarSyncParser() throws IOException, RemoteException, CommandStatusException {
        // Setup some messages
        ArrayList<Long> messageIds = setupAccountMailboxAndMessages(3);
        byte[] bt = "1234".getBytes();
        String string = Base64.encodeToString(bt, Base64.NO_WRAP);
        // Setup our adapter and parser
        setupSyncParserAndAdapter(mAccount, mMailbox);
        Serializer s = new Serializer();
        s.start(Tags.SYNC_SYNC).start(Tags.SYNC_COMMANDS).tag(Tags.SYNC_SYNC).start(Tags.SYNC_ADD)
        .tag(Tags.SYNC_SYNC).data(Tags.SYNC_SERVER_ID, "serverid").tag(Tags.SYNC_APPLICATION_DATA)
        .end().start(Tags.SYNC_CHANGE).tag(Tags.SYNC_SYNC).data(Tags.SYNC_SERVER_ID, "serverid")
        .start(Tags.SYNC_APPLICATION_DATA).data(Tags.CALENDAR_BODY, "todo")
        .data(Tags.CALENDAR_RESPONSE_TYPE, "1").start(Tags.CALENDAR_CATEGORIES)
        .tag(Tags.CALENDAR).data(Tags.CALENDAR_CATEGORY, "work").end()
        .data(Tags.CALENDAR_REMINDER_MINS_BEFORE, "5").data(Tags.CALENDAR_TIME_ZONE, string)
        .data(Tags.CALENDAR_START_TIME, "20121111T180303Z").data(Tags.CALENDAR_ALL_DAY_EVENT, "1")
        .data(Tags.CALENDAR_ORGANIZER_EMAIL, "123@a.com").data(Tags.CALENDAR_ORGANIZER_NAME, "mtk")
        .start(Tags.CALENDAR_ATTACHMENTS) .tag(Tags.CALENDAR).start(Tags.CALENDAR_ATTACHMENT)
        .tag(Tags.CALENDAR).end().end().start(Tags.CALENDAR_ATTENDEES).tag(Tags.CALENDAR)
        .start(Tags.CALENDAR_ATTENDEE).tag(Tags.CALENDAR).data(Tags.CALENDAR_ATTENDEE_STATUS, "6")
        .data(Tags.CALENDAR_ATTENDEE_TYPE, "2").end().end().start(Tags.CALENDAR_EXCEPTIONS)
        .tag(Tags.CALENDAR).start(Tags.CALENDAR_EXCEPTION)
        .tag(Tags.CALENDAR_ATTACHMENTS).data(Tags.CALENDAR_EXCEPTION_START_TIME, "20121111T180303Z")
        .data(Tags.CALENDAR_EXCEPTION_IS_DELETED, "1").data(Tags.CALENDAR_ALL_DAY_EVENT, "1")
        .tag(Tags.BASE_BODY).data(Tags.CALENDAR_BODY, "calendar")
        .data(Tags.CALENDAR_START_TIME, "20121111T180303Z")
        .data(Tags.CALENDAR_END_TIME, "20121112T180303Z")
        .data(Tags.CALENDAR_LOCATION, "HK").data(Tags.CALENDAR_SUBJECT, "work")
        .data(Tags.CALENDAR_SENSITIVITY, "1").data(Tags.CALENDAR_BUSY_STATUS, "0")
        .tag(Tags.CALENDAR).start(Tags.CALENDAR_RECURRENCE).tag(Tags.CALENDAR)
        .data(Tags.CALENDAR_RECURRENCE_TYPE, "1").data(Tags.CALENDAR_RECURRENCE_INTERVAL, "5")
        .data(Tags.CALENDAR_RECURRENCE_OCCURRENCES, "2")
        .data(Tags.CALENDAR_RECURRENCE_DAYOFWEEK, "2")
        .data(Tags.CALENDAR_RECURRENCE_DAYOFMONTH, "11")
        .data(Tags.CALENDAR_RECURRENCE_WEEKOFMONTH, "5")
        .data(Tags.CALENDAR_RECURRENCE_MONTHOFYEAR, "3")
        .data(Tags.CALENDAR_RECURRENCE_UNTIL, "dead")
        .end().end().end().end().end().start(Tags.SYNC_DELETE).tag(Tags.SYNC_SYNC)
        .data(Tags.SYNC_SERVER_ID, "serverid").end().end().start(Tags.SYNC_RESPONSES)
        .tag(Tags.SYNC_SYNC).start(Tags.SYNC_ADD).tag(Tags.SYNC_SYNC)
        .data(Tags.SYNC_SERVER_ID, "serverid").data(Tags.SYNC_STATUS, "2")
        .data(Tags.SYNC_CLIENT_ID, "clientid").end().start(Tags.SYNC_CHANGE)
        .tag(Tags.SYNC_SYNC).data(Tags.SYNC_SERVER_ID, "serverid").data(Tags.SYNC_STATUS, "1")
        .end().end().end().done();
        ByteArrayInputStream btStream = new ByteArrayInputStream(s.toByteArray());
        assertNotNull(btStream);
        boolean result = mAdapter.parse(btStream);
        assertFalse(result);
    }

    private String[] getAccountArgument(long id) {
        ACCOUNT_ARGUMENT[0] = Long.toString(id);
        return ACCOUNT_ARGUMENT;
    }

    void setupSyncParserAndAdapter(Account account, Mailbox mailbox) throws IOException {
        EasSyncService service = getTestService(account, mailbox);
        mAdapter = new CalendarSyncAdapter(service);
        assertNotNull(mAdapter);
    }

    ArrayList<Long> setupAccountMailboxAndMessages(int numMessages) {
        ArrayList<Long> ids = new ArrayList<Long>();

        // Create account and two mailboxes
        mAccount = EmailContentSetupUtils.setupEasAccount("account", true, mProviderContext);
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

    public void testSendLocalChanges() throws IOException, RemoteException {
        Configuration.openTest();
        EasSyncService service = getTestService();
        Account account = setupProviderAndAccountManagerAccount("test");
        service.mAccount = account;
        String syncKey = "sync-key";
        mAdapter = new CalendarSyncAdapter(service);
        ContentValues cv = new ContentValues();
        cv.put(Calendars._ID, 0);
        cv.put(Calendars.ACCOUNT_NAME, account.mEmailAddress);
        cv.put(Calendars.ACCOUNT_TYPE, Eas.EXCHANGE_ACCOUNT_MANAGER_TYPE);
        cv.put(Calendars.NAME, "lucy");
        try {
            mAdapter.mContentResolver.insert(CalendarSyncAdapter.asSyncAdapter(
                    Calendars.CONTENT_URI,
                    mAdapter.mService.mAccount.mEmailAddress,
                    Eas.EXCHANGE_ACCOUNT_MANAGER_TYPE), cv);
            addEvent(true);
            addExtendedProperties(true);
            ContentProviderClient client = mAdapter.mContentResolver
                    .acquireContentProviderClient(CalendarContract.CONTENT_URI);
            SyncStateContract.Helpers.set(client, CalendarSyncAdapter
                    .asSyncAdapter(SyncState.CONTENT_URI,
                            mAdapter.mService.mAccount.mEmailAddress,
                            Eas.EXCHANGE_ACCOUNT_MANAGER_TYPE),
                    mAdapter.mAccountManagerAccount, syncKey.getBytes());
            syncKey = mAdapter.getSyncKey();
            Serializer serializer = new Serializer();
            mAdapter.mService.mProtocolVersionDouble = 12.0;
            mAdapter.sendLocalChanges(serializer);
            addEvent(true);
            addExtendedProperties(true);
            addOriginalSyncId();
            mAdapter.sendLocalChanges(serializer);
            addEvent(false);
            addExtendedProperties(false);
            addAttendees();
            mAdapter.sendLocalChanges(serializer);
            EntityIterator exIterator = EventsEntity.newEntityIterator(
                    mResolver.query(CalendarSyncAdapter.asSyncAdapter(
                            Events.CONTENT_URI,
                            mAdapter.mService.mAccount.mEmailAddress,
                            Eas.EXCHANGE_ACCOUNT_MANAGER_TYPE), null,
                            Events.ORIGINAL_SYNC_ID + "=? AND "
                                    + Events.CALENDAR_ID + "=?", new String[] {
                                    "sync_id", "2" }, null), mResolver);
            boolean exFirst = true;
            if (exIterator.hasNext()) {
                exFirst = false;
            }
            assertFalse(exFirst);
        } finally {
            Configuration.shutDownTest();
        }
    }

    protected android.accounts.Account[] getExchangeAccounts() {
        return AccountManager.get(getContext()).getAccountsByType(
                Eas.EXCHANGE_ACCOUNT_MANAGER_TYPE);
    }

    protected android.accounts.Account makeAccountManagerAccount(String username) {
        return new android.accounts.Account(username, Eas.EXCHANGE_ACCOUNT_MANAGER_TYPE);
    }

    protected void createAccountManagerAccount(String username) {
        final android.accounts.Account account = makeAccountManagerAccount(username);
        AccountManager.get(getContext()).addAccountExplicitly(account,
                "password", null);
    }

    protected Account setupProviderAndAccountManagerAccount(String username) {
        // Note that setupAccount creates the email address
        // username@android.com, so that's what
        // we need to use for the account manager
        createAccountManagerAccount(username + TEST_ACCOUNT_SUFFIX);
        return EmailContentSetupUtils.setupAccount(username, true,
                mProviderContext);
    }

    protected void addEvent(boolean hasServerId) {
        ContentValues eValues  = new ContentValues();
        Calendar calendar = Calendar.getInstance();
        calendar.set(2014, 11, 9, 11, 0);
        long startMillis = calendar.getTimeInMillis();
        calendar.set(2014, 11, 10, 11, 0);
        long endMillis = calendar.getTimeInMillis();
        String rrule = "FREQ=MONTHLY;UNTIL=20141110T110000Z;WKST=SU;BYMONTHDAY=15";
        if (hasServerId) {
            eValues.put(Events._ID, 1);
            eValues.put(Events._SYNC_ID, "sync_id");
            eValues.put(Events.ORGANIZER, "tom@a.com");
            eValues.put(Events.SYNC_DATA4, "2");
        } else {
            eValues.put(Events._ID, 2);
            eValues.put(Events.ORGANIZER, "test@android.com");
        }
        eValues.put(Events.CALENDAR_ID, 2);
        eValues.put(Events.TITLE, "testcase");
        eValues.put(Events.DESCRIPTION, "test");
        eValues.put(Events.EVENT_LOCATION, "chengdu");
        eValues.put(Events.STATUS, 2);
        eValues.put(Events.SELF_ATTENDEE_STATUS, 1);
        eValues.put(Events.DTSTART, startMillis);
        eValues.put(Events.DTEND, endMillis);
        eValues.put(Events.DURATION, "P1D");
        eValues.put(Events.EVENT_END_TIMEZONE, "GMT+8");
        eValues.put(Events.HAS_ALARM, 1);
        eValues.put(Events.RRULE, rrule);
        eValues.put(Events.ORIGINAL_SYNC_ID, "sync_id");
        eValues.put(Events.DIRTY, 1);
        mAdapter.mContentResolver.insert(CalendarSyncAdapter.asSyncAdapter(Events.CONTENT_URI,
                mAdapter.mService.mAccount.mEmailAddress,
                Eas.EXCHANGE_ACCOUNT_MANAGER_TYPE), eValues);
    }

    protected void addOriginalSyncId() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2014, 11, 9, 11, 0);
        long startMillis = calendar.getTimeInMillis();
        calendar.set(2014, 11, 10, 11, 0);
        long endMillis = calendar.getTimeInMillis();
        ContentValues eValues  = new ContentValues();
        eValues.put(Events._ID, 3);
        eValues.put(Events.CALENDAR_ID, 2);
        eValues.put(Events.DTSTART, startMillis);
        eValues.put(Events.DTEND, endMillis);
        eValues.put(Events.ORIGINAL_INSTANCE_TIME, startMillis);
        eValues.put(Events.ORIGINAL_SYNC_ID, "sync_id");
        eValues.put(Events._SYNC_ID, "sync_id");
        eValues.put(Events.EVENT_END_TIMEZONE, "GMT+8");
        eValues.put(Events.DIRTY, 1);
        eValues.put(Events.STATUS, 2);
        mAdapter.mContentResolver.insert(CalendarSyncAdapter.asSyncAdapter(Events.CONTENT_URI,
                mAdapter.mService.mAccount.mEmailAddress,
                Eas.EXCHANGE_ACCOUNT_MANAGER_TYPE), eValues);
    }
    protected void addExtendedProperties(boolean hasServerId) {
        ContentValues eValues  = new ContentValues();
        if (hasServerId) {
            eValues.put(ExtendedProperties.NAME, "userAttendeeStatus");
            eValues.put(ExtendedProperties.EVENT_ID, 1);
            eValues.put(ExtendedProperties._ID, 1);
        } else {
            eValues.put(ExtendedProperties.NAME, "attendees");
            eValues.put(ExtendedProperties.EVENT_ID, 2);
            eValues.put(ExtendedProperties._ID, 2);
        }
        eValues.put(ExtendedProperties.VALUE, "2");
        mAdapter.mContentResolver.insert(CalendarSyncAdapter.asSyncAdapter(
                ExtendedProperties.CONTENT_URI,
                mAdapter.mService.mAccount.mEmailAddress,
                Eas.EXCHANGE_ACCOUNT_MANAGER_TYPE), eValues);
    }

    protected void addAttendees() {
        ContentValues eValues  = new ContentValues();
        eValues.put(Attendees.ATTENDEE_EMAIL, "attendees@a.com");
        eValues.put(Attendees.EVENT_ID, 2);
        mAdapter.mContentResolver.insert(CalendarSyncAdapter.asSyncAdapter(
                Attendees.CONTENT_URI,
                mAdapter.mService.mAccount.mEmailAddress,
                Eas.EXCHANGE_ACCOUNT_MANAGER_TYPE), eValues);
    }

    public void testSetSyncKeyAndSendSyncOptions() throws Exception {
        setupAccountMailboxAndMessages(0);
        setupSyncParserAndAdapter(mAccount, mMailbox);
        mAdapter.setSyncKey("syncKey", true);
        assertEquals("0", mAdapter.getSyncKey());
        mAdapter.setSyncKey("syncKey", false);
        assertEquals("syncKey", mAdapter.getSyncKey());
        Serializer s = new Serializer();
        mAdapter.sendSyncOptions(2.5, s, Eas.EAS_SYNC_NORMAL);
        Serializer expected = new Serializer();
        expected.tag(Tags.SYNC_DELETES_AS_MOVES);
        expected.tag(Tags.SYNC_GET_CHANGES);
        expected.data(Tags.SYNC_WINDOW_SIZE, "20");
        expected.start(Tags.SYNC_OPTIONS);
        expected.data(Tags.SYNC_FILTER_TYPE, "4");
        expected.data(Tags.SYNC_TRUNCATION, Eas.EAS2_5_TRUNCATION_SIZE);
        expected.end();
        assertEquals(expected.toString(), s.toString());
        assertFalse(mAdapter.isSyncable());
    }

    @Override
    public void tearDown() throws Exception {
        android.accounts.Account[] accounts = getExchangeAccounts();
        for (android.accounts.Account acct : accounts) {
            AccountManager.get(getContext()).removeAccount(acct, null, null);
        }
        super.tearDown();
    }
}
