package com.android.exchange;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.android.emailcommon.Configuration;
import com.android.emailcommon.Logging;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.provider.EmailContent.Message;
import com.android.emailcommon.service.EmailExternalCalls;
import com.android.emailcommon.service.EmailExternalConstants;
import com.android.exchange.EasResponse;
import com.android.exchange.adapter.MockEasResponse;
import com.android.exchange.provider.EmailContentSetupUtils;
import com.android.exchange.utility.ExchangeTestCase;
import com.android.exchange.utility.TestUtils;

@SmallTest
public class ExchangeServiceTest2 extends AndroidTestCase {
    private Context mProviderContext;
    private static final String TEST_ACCOUNT_NAME = "EasAccountService";
    private static final String TEST_ACCOUNT_PWD = "password";
    private Account mAccount;
    private Mailbox mMailBox;
    private Mailbox mContactMailBox;
    private Mailbox mCalendarMailBox;
    private Message mMessage;
    private int mResult = -3;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mProviderContext = getContext();
        Configuration.openTest();
    }

    @Override
    protected void tearDown() throws Exception {
        Configuration.shutDownTest();
        super.tearDown();
    }

    /**
     * Setup testcase environment (Accout + MailBox).
     */
    private void setUpAccountAndMailBox(boolean send) {
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
                mProviderContext, send ? Mailbox.TYPE_OUTBOX
                        : Mailbox.TYPE_EAS_ACCOUNT_MAILBOX);
        mMailBox.mSyncInterval = Mailbox.CHECK_INTERVAL_PUSH;
        mMailBox.mServerId = "_1:1";
        mMailBox.mSyncStatus = "S3:0:0";
        mMailBox.save(mProviderContext);

        mMessage = EmailContentSetupUtils.setupMessage(
                "MockMessage-ExchangeService", mAccount.mId, mMailBox.mId,
                true, true, mProviderContext);
        // Setup ContactMailBox
        mContactMailBox = EmailContentSetupUtils.setupMailbox(
                "EasAccountServiceTests-contact", mAccount.mId, true,
                mProviderContext, Mailbox.TYPE_CONTACTS);

        // Setup CalendarMailBox
        mCalendarMailBox = EmailContentSetupUtils.setupMailbox(
                "EasAccountServiceTests-contact", mAccount.mId, true,
                mProviderContext, Mailbox.TYPE_CALENDAR);

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

    public void testExchangeAccount() {
        setUpAccountAndMailBox(false);
        try {
            assertNotNull("testDeleteAccountPIMData-account ", mAccount);
            ExchangeTestCase.startExchangeService(mProviderContext);
            TestUtils.sleepAndWait(TestUtils.WAIT_FOR_SERVER_START);
            ExchangeService mockService = ExchangeService.INSTANCE;
            // Start Thread
            mockService.maybeStartExchangeServiceThread();
            // Start Service
            ExchangeService.checkExchangeServiceServiceRunning();

            // Difficult check test result (wipe data)
            ExchangeService.deleteAccountPIMData(mAccount.mId);

            Account a = ExchangeService.getAccountById(mAccount.mId);
            if (a == null) {
                mockService.mAccountList.add(mAccount);
            }
            a = ExchangeService.getAccountById(mAccount.mId);
            assertNotNull(a);

            a = ExchangeService.getAccountByName(mAccount.mEmailAddress);
            assertNotNull(a);

            String result = "accountKey in (" + mAccount.mId + ")";
            String select = ExchangeService.getEasAccountSelector();
            assertEquals("ExchangeService-AccountSelector", result, select);

            ExchangeService.reloadFolderList(mProviderContext, mAccount.mId,
                    true);

        } finally {
            removeAccountAndMailBox();
        }
    }

    public void testStaticMethods() {
        setUpAccountAndMailBox(false);
        try {
            String input = null;
            int result = ExchangeService.getStatusType(input);
            assertEquals(-1, result);

            input = "S3:0:0";
            result = ExchangeService.getStatusType(input);
            assertEquals(3, result);

            input = "S3:0:7";
            result = ExchangeService.getStatusChangeCount(input);
            assertEquals(7, result);

            input = "S3:0:S";
            result = ExchangeService.getStatusChangeCount(input);
            assertEquals(-1, result);

        } finally {
            removeAccountAndMailBox();
        }
    }

    public void testSendMessageRequest() {
        setUpAccountAndMailBox(false);
        try {
            assertNotNull("testSendMessageRequest-account ", mAccount);
            ExchangeTestCase.startExchangeService(mProviderContext);
            // Wait some moment. Condition: ExchangeService.INSTANCE !=null;
            TestUtils.waitUntil("EasAccountServiceTests-Wait",
                    new TestUtils.Condition() {
                        @Override
                        public boolean isMet() {
                            return (ExchangeService.INSTANCE != null);
                        }
                    }, 5);

            ExchangeService mockService = ExchangeService.INSTANCE;
            // Start Thread
            mockService.maybeStartExchangeServiceThread();
            // Start Service
            ExchangeService.checkExchangeServiceServiceRunning();
            assertNotNull(mMessage);
            ExchangeService.sendMessageRequest(new MockRequest(mMessage.mId));

            assertNotNull(mMailBox);
            mockService
                    .startSyncMailBoxForBT(mMailBox.mId, new MockCallbacks());

//            ExchangeService.accountUpdated(mAccount.mId);

//            ExchangeService.releaseSecurityHold(mAccount);
        } finally {
            removeAccountAndMailBox();
        }
    }

    public void testSendMessage() {
        setUpAccountAndMailBox(true);
        EasResponse.sCallback = new MockEasResponse();
        try {
            assertNotNull("testSendMessage-account ", mAccount);
            // Should receive EmailExternalConstants.RESULT_FAIL call back.
            ExchangeTestCase.startExchangeService(mProviderContext);
            // Wait some moment. Condition: ExchangeService.INSTANCE !=null;
            TestUtils.waitUntil("EasAccountServiceTests-Wait",
                    new TestUtils.Condition() {
                        @Override
                        public boolean isMet() {
                            return (ExchangeService.INSTANCE != null);
                        }
                    }, 5);

            ExchangeService mockService = ExchangeService.INSTANCE;
            // Start Thread
            mockService.maybeStartExchangeServiceThread();
            // Start Service
            ExchangeService.checkExchangeServiceServiceRunning();
            assertNotNull(mMessage);

            mockService.sendMessages(mAccount.mId, mMailBox.mId, TestUtils
                    .createAvailableUri(), new MockCallbacks(), true);
            TestUtils.waitUntil("EasAccountServiceTests-Wait",
                    new TestUtils.Condition() {
                        @Override
                        public boolean isMet() {
                            return mResult != -3;
                        }
                    }, 5);
            assertTrue("Result-1 ", (mResult >=0));
            // Reset the callback values waiting for next result.
            mResult = -3;

            // What if the mailbox id is -1?
            mockService.sendMessages(mAccount.mId, 9999, TestUtils
                    .createAvailableUri(), new MockCallbacks(), true);
            TestUtils.waitUntil("EasAccountServiceTests-Wait",
                    new TestUtils.Condition() {
                        @Override
                        public boolean isMet() {
                            return mResult != -99999;
                        }
                    }, 5);
            assertTrue("Result-2 ", (mResult >=0));

        } finally {
            removeAccountAndMailBox();
            EasResponse.sCallback = null;
        }
    }

    public void testIsMailboxSyncable() {
        setUpAccountAndMailBox(false);
        try {
            assertNotNull("testSendMessage-account ", mAccount);
            // Should receive EmailExternalConstants.RESULT_FAIL call back.
            ExchangeTestCase.startExchangeService(mProviderContext);
            // Wait some moment. Condition: ExchangeService.INSTANCE !=null;
            TestUtils.waitUntil("EasAccountServiceTests-Wait",
                    new TestUtils.Condition() {
                        @Override
                        public boolean isMet() {
                            return (ExchangeService.INSTANCE != null);
                        }
                    }, 5);

            ExchangeService mockService = ExchangeService.INSTANCE;
            assertNotNull(mockService);
            Configuration.shutDownTest();
            // Turn on Auto-Sync.
            ContentResolver.setMasterSyncAutomatically(true);
            // Check TYPE_INBOX return true;
            boolean result;
            result = mockService.testIsMailboxSyncable(mAccount,
                    Mailbox.TYPE_OUTBOX);
            assertTrue("testIsMailboxSyncable-0", result);

            result = mockService.testIsMailboxSyncable(mAccount,
                    Mailbox.TYPE_CONTACTS);
            assertFalse("testIsMailboxSyncable-0", result);

            result = mockService.testIsMailboxSyncable(mAccount,
                    Mailbox.TYPE_CALENDAR);
            assertFalse("testIsMailboxSyncable-0", result);

            result = mockService.testIsMailboxSyncable(mAccount,
                    Mailbox.TYPE_TRASH);
            assertFalse("testIsMailboxSyncable-0", result);
        } finally {
            removeAccountAndMailBox();
        }
    }

    public void testAlertAndWakeLock() {
        setUpAccountAndMailBox(false);
        try {
            assertNotNull("testSendMessage-account ", mAccount);
            // Should receive EmailExternalConstants.RESULT_FAIL call back.
            ExchangeTestCase.startExchangeService(mProviderContext);
            // Wait some moment. Condition: ExchangeService.INSTANCE !=null;
            TestUtils.waitUntil("EasAccountServiceTests-Wait",
                    new TestUtils.Condition() {
                        @Override
                        public boolean isMet() {
                            return (ExchangeService.INSTANCE != null);
                        }
                    }, 5);

            ExchangeService mockService = ExchangeService.INSTANCE;
            // Start Thread
            mockService.maybeStartExchangeServiceThread();
            // Start Service
            ExchangeService.checkExchangeServiceServiceRunning();

            long mailBoxId = mMailBox.mId;
            int size0 = mockService.getPendingIntents().size();
            ExchangeService.setEasSyncAlarm(mailBoxId, 1000);
            int size1 = mockService.getPendingIntents().size();
            assertTrue("setEasSyncAlarm", (size1 == size0 + 1));
            TestUtils.sleepAndWait();
            ExchangeService.clearEasSyncAlarm(mailBoxId);
            ExchangeService.alert(mProviderContext, mailBoxId);

            ExchangeService.acquireEasWakeLock(mailBoxId);
            assertNotNull(mockService.getWakeLock().get(mailBoxId));
            ExchangeService.releaseEasWakeLock(mailBoxId);

        } finally {
            removeAccountAndMailBox();
        }
    }

    public void testFetchMailRequest() {
        setUpAccountAndMailBox(false);
        try {
            assertNotNull("testSendMessage-account ", mAccount);
            // Should receive EmailExternalConstants.RESULT_FAIL call back.
            ExchangeTestCase.startExchangeService(mProviderContext);
            // Wait some moment. Condition: ExchangeService.INSTANCE !=null;
            TestUtils.waitUntil("EasAccountServiceTests-Wait",
                    new TestUtils.Condition() {
                        @Override
                        public boolean isMet() {
                            return (ExchangeService.INSTANCE != null);
                        }
                    }, 5);

            ExchangeService mockService = ExchangeService.INSTANCE;
            assertNotNull(mockService);
            // Start Thread
            mockService.maybeStartExchangeServiceThread();
            // Start Service
            ExchangeService.checkExchangeServiceServiceRunning();

            assertNotNull(mMessage);
            ExchangeService.fetchMailRequest(new MockRequest(mMessage.mId));
            ExchangeService.serviceRequestImmediate(mMailBox.mId);
            // TODO How to check the result?

        } finally {
            removeAccountAndMailBox();
        }
    }

    private class MockCallbacks extends EmailExternalCalls.Stub {

        public void sendCallback(int result, long accountId, int resultType) {
            mResult = result;
        }

        public void updateCallback(int result, long accountId, long mailboxId) {
        }
    }

    public class MockRequest extends Request {

        public MockRequest(long messageId) {
            super(messageId);
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }
}
