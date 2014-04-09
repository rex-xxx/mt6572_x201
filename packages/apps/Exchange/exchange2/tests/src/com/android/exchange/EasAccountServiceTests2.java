package com.android.exchange;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.ContentResolver;
import android.content.Context;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.android.emailcommon.Configuration;
import com.android.emailcommon.Logging;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.Mailbox;
import com.android.exchange.adapter.MockEasResponse;
import com.android.exchange.adapter.Serializer;
import com.android.exchange.adapter.Tags;
import com.android.exchange.provider.EmailContentSetupUtils;
import com.android.exchange.utility.MockHttpURLConnection;
import com.android.exchange.utility.TestUtils;

/**
 * TestCase for com.android.exchange.EasAccountServiceTests exchange
 */
@SmallTest
public class EasAccountServiceTests2 extends AndroidTestCase {

    private Context mProviderContext;
    private static final String TEST_ACCOUNT_NAME = "EasAccountService";
    private static final String TEST_ACCOUNT_PWD = "password";
    private Account mAccount;
    private Mailbox mMailBox;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mProviderContext = getContext();
        Configuration.openTest();
        EasResponse.sCallback = new MockEasResponse();
    }

    @Override
    protected void tearDown() throws Exception {
        Configuration.shutDownTest();
        EasResponse.sCallback = null;
        super.tearDown();
    }

    /**
     * Sync account with mailbox.
     */
    public void testSync() {
        try {
            TestUtils.sleepAndWait(TestUtils.WAIT_FOR_LONG_LOADING);
            setUpAccountAndMailBox();
            assertNotNull(mAccount);
            assertNotNull(mMailBox);
            // Setup EasAccountService
            final EasAccountService easAccountService = new EasAccountService(
                    mProviderContext, mMailBox);
            easAccountService.mMailbox = mMailBox;
            easAccountService.mAccount = mAccount;
            easAccountService.setMockConnection(getMockHttpConnection());

            // It will run FolderSync and return the success in the MockResponse
            Thread thread = new Thread(easAccountService,
                    "[EasAccountServiceTests-0]");
            thread.start();
            assertNotNull(easAccountService);
            TestUtils.waitUntil("EasAccountServiceTests-Wait",
                    new TestUtils.Condition() {
                        @Override
                        public boolean isMet() {
                            return (easAccountService.getPingResponseCode() != 0);
                        }
                    }, 5);

            // After finish sync, check mProtocolVersion and mExitStatus
            assertEquals("[EasAccountServiceTests-mExitStatus",
                    EasSyncService.EXIT_DONE, easAccountService.mExitStatus);
            assertEquals("[EasAccountServiceTests-mProtocolVersion", "14.1",
                    easAccountService.mProtocolVersion);
            assertEquals("[EasAccountServiceTests-mProtocolVersionDouble",
                    14.1d, easAccountService.mProtocolVersionDouble);
        } finally {
            removeAccountAndMailBox();
        }
    }

    /**
     * Test Send MockPing and check ping result code.
     */
    public void testPing() {
        try {
            // Setup an exchange account
            setUpAccountAndMailBox();
            assertNotNull(mAccount);
            assertNotNull(mMailBox);

            // Setup EasAccountService
            final EasAccountService easAccountService = new EasAccountService(
                    mProviderContext, mMailBox);
            easAccountService.mMailbox = mMailBox;
            easAccountService.mAccount = mAccount;
            easAccountService.setMockConnection(getMockHttpConnection());

            boolean result = easAccountService.setupService();
            assertTrue(result);

            Serializer s = getMockPing();
            if (s != null) {
                try {
                    easAccountService.sendPing(s.toByteArray(), 100);
                    TestUtils.waitUntil("EasAccountServiceTests-1",
                            new TestUtils.Condition() {
                                @Override
                                public boolean isMet() {
                                    return (easAccountService
                                            .getPingResponseCode() == HttpURLConnection.HTTP_OK);
                                }
                            }, 5);
                    assertEquals("testPing-result ", HttpURLConnection.HTTP_OK,
                            easAccountService.getPingResponseCode());
                } catch (IOException e) {
                }
            }

            assertNotNull(easAccountService.mAccount);
            // Test HandleAccountMailboxRedirect
            EasResponse mockRs = new MockEasResponse();
            boolean redirectResult = easAccountService
                    .testHandleAccountMailboxRedirect(mockRs);
            assertTrue("AccountMailboxRedirect ", redirectResult);

            // Test pushFallback, for Inbox set mSyncInterval as 5, other 1.
            assertNotNull(mMailBox);
            easAccountService.testPushFallback(mMailBox.mId);
            TestUtils.sleepAndWait(TestUtils.WAIT_FOR_DB_OPERATION);
            mMailBox = Mailbox.restoreMailboxWithId(mProviderContext,
                    mMailBox.mId);
            // result will be 5 or -2, never be -1.
            assertTrue("testPushFallback", mMailBox.mSyncInterval != -1);

            String message = null;
            boolean failure = easAccountService.testIsLikelyNatFailure(message);
            assertFalse(failure);

            message = "false";
            failure = easAccountService.testIsLikelyNatFailure(message);
            assertFalse(failure);

            message = "reset by peer";
            failure = easAccountService.testIsLikelyNatFailure(message);
            assertTrue(failure);

        } finally {
            removeAccountAndMailBox();
        }
    }

    /**
     * Test Parse MockPing and check parse result.
     */
    public void testParsePing() {
        try {
            // Setup an exchange account
            setUpAccountAndMailBox();
            assertNotNull(mAccount);
            assertNotNull(mMailBox);

            // Setup EasAccountService
            final EasAccountService easAccountService = new EasAccountService(
                    mProviderContext, mMailBox);
            easAccountService.mMailbox = mMailBox;
            easAccountService.mAccount = mAccount;

            HashMap<String, Integer> errorMap = new HashMap<String, Integer>();
            easAccountService.testParsePing(getMockPingResult(),
                    mProviderContext.getContentResolver(), errorMap);

            // Parse result should contain 1 changed folder.
            ArrayList<String> changeList = easAccountService
                    .getPingChangeList();
            assertNotNull("testParsePing-changeList ", changeList);
            assertEquals("testParsePing-size ", 1, changeList.size());

        } catch (StaleFolderListException e) {
        } catch (IllegalHeartbeatException e) {
        } catch (CommandStatusException e) {
        } catch (IOException e) {
        } finally {
            removeAccountAndMailBox();
        }
    }

    /**
     * Sync account with mailbox make the exception happened
     */
    public void testSyncFailed() {
        try {
            TestUtils.sleepAndWait(TestUtils.WAIT_FOR_LONG_LOADING);

            setUpAccountAndMailBox();
            assertNotNull(mAccount);
            assertNotNull(mMailBox);
            // Setup EasAccountService
            final EasAccountService easAccountService = new EasAccountService(
                    mProviderContext, mMailBox);
            easAccountService.mMailbox = mMailBox;
            easAccountService.mAccount = mAccount;

            // It will run FolderSync and return the success in the MockResponse
            Thread thread = new Thread(easAccountService,
                    "[EasAccountServiceTests-0]");
            thread.start();
            assertNotNull(easAccountService);

            // Not inject mock HttpConnection, if no available network connection.
            // Note: we can't get a consistent mExitStatus for:
            // 1) network unavailable,  mExitStatus = EXIT_IO_ERROR
            // 2) network available, mExitStatus = EXIT_DONE
            TestUtils.waitUntil("EasAccountServiceTests-Wait",
                    new TestUtils.Condition() {
                        @Override
                        public boolean isMet() {
                            return (easAccountService.mExitStatus == EasAccountService.EXIT_DONE);
                        }
                    }, 5);

            // After finish sync, check mProtocolVersion and mExitStatus
            assertTrue(
                    "[EasAccountServiceTests-mExitStatus",
                    easAccountService.mExitStatus != EasAccountService.EXIT_EXCEPTION);
        } finally {
            removeAccountAndMailBox();
        }
    }

    /**
     * Create a Mock Ping request.
     */
    private Serializer getMockPing() {
        Serializer s = null;
        try {
            s = new Serializer();
            s.start(Tags.PING_PING).data(Tags.PING_HEARTBEAT_INTERVAL, "100")
                    .start(Tags.PING_FOLDERS).start(Tags.PING_FOLDER).data(
                            Tags.PING_ID, "key-0")
                    .data(Tags.PING_CLASS, "mail").end().end().done();
        } catch (IOException e) {
        }
        return s;
    }

    /**
     * Create a Mock Ping response.
     */
    private InputStream getMockPingResult() throws IOException {
        Serializer s = new Serializer();
        s.start(Tags.PING_PING).data(Tags.PING_STATUS, "2").tag(Tags.PING_PING)
                .start(Tags.PING_FOLDERS).data(Tags.PING_FOLDER, "_1:1").tag(
                        Tags.PING_PING).end().end().done();

        byte[] bytes = s.toByteArray();
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        return bis;
    }

    /**
     * Create a Mock HttpConnection to send Ping.
     */
    private MockHttpURLConnection getMockHttpConnection() {
        URL url = null;
        try {
            url = new URL("http:\\www.google.com");
        } catch (MalformedURLException e) {
        }
        return new MockHttpURLConnection(url);
    }

    /**
     * M: Setup testcase environment (Accout + MailBox).
     */
    private void setUpAccountAndMailBox() {
        // Setup account
        mAccount = EmailContentSetupUtils.setupEasAccount(TEST_ACCOUNT_NAME,
                false, mProviderContext);
        mAccount.mSyncInterval = Account.CHECK_INTERVAL_PUSH;
        mAccount.mFlags |= Account.FLAGS_SUPPORTS_SEARCH;
        mAccount.mSyncKey = "1";
        mAccount.save(mProviderContext);

        // Setup folder
        mMailBox = EmailContentSetupUtils.setupMailbox(
                "EasAccountServiceTests-Ping", mAccount.mId, false,
                mProviderContext, Mailbox.TYPE_INBOX);
        mMailBox.mSyncInterval = Mailbox.CHECK_INTERVAL_PUSH;
        mMailBox.mServerId = "_1:1";
        mMailBox.mSyncStatus = "S3:0:0";
        mMailBox.save(mProviderContext);

        EmailContentSetupUtils.createAccountManagerAccount(TEST_ACCOUNT_NAME,
                TEST_ACCOUNT_PWD, mProviderContext);
    }

    /**
     * Clear testcase environment (Accout + MailBox).
     */
    private void removeAccountAndMailBox() {
        if (mAccount != null) {
            ContentResolver resolver = mProviderContext.getContentResolver();
            resolver.delete(mAccount.getUri(), null, null);
        }
        EmailContentSetupUtils.deleteTemporaryAccountManagerAccounts(
                TEST_ACCOUNT_NAME, mProviderContext);
    }
}
