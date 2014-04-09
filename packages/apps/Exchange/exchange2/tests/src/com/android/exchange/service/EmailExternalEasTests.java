package com.android.exchange.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import android.accounts.Account;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import com.android.emailcommon.Configuration;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.service.EmailExternalCalls;
import com.android.emailcommon.service.EmailExternalConstants;
import com.android.exchange.EasResponse;
import com.android.exchange.ExchangeService;
import com.android.exchange.adapter.MockEasResponse;
import com.android.exchange.provider.EmailContentSetupUtils;
import com.android.exchange.utility.ExchangeTestCase;
import com.android.exchange.utility.TestUtils;

/**
 * M: Testcase for EmailExternalEas to send BTMAP message.
 */
public class EmailExternalEasTests extends ExchangeTestCase {

    private final EmailExternalCalls mEmailExternalCalls = new ControllerCallbacks();
    private int mResult = 0;
    private boolean mReceiveCallBack = false;
    private static final String TEST_ACCOUNT_NAME = "eas_external";
    private static final String TEST_ACCOUNT_PWD = "password";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mReceiveCallBack = false;
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testSendMessage() {
        Configuration.openTest();
        EasResponse.sCallback = new MockEasResponse();
        try {
            // Setup an exchange account
            com.android.emailcommon.provider.Account account = setupTestEasAccount(
                    TEST_ACCOUNT_NAME, true);
            // Setup folder
            Mailbox mailbox = EmailContentSetupUtils.setupMailbox("BTMAP_Test",
                    account.mId, true, mProviderContext, Mailbox.TYPE_INBOX);
            Account sysAccount = createAccountManagerAccount(TEST_ACCOUNT_NAME,
                    TEST_ACCOUNT_PWD);
            // Start exchange service
            startExchangeService(mProviderContext);
            TestUtils.sleepAndWait(TestUtils.WAIT_FOR_DB_OPERATION);

            // Setup EmailExternalEas and try send message.
            // 1) Test available eml file
            EmailExternalEas externalEas = new EmailExternalEas(
                    mProviderContext, mailbox, TestUtils.createAvailableUri(),
                    mEmailExternalCalls, true);
            externalEas.mAccount = account;
            externalEas.mMailbox = mailbox;
            Thread thread = new Thread(externalEas, "[BTMAP_Test-0]");
            thread.start();
            TestUtils.sleepAndWait(1000);

            TestUtils.waitUntil("Wait_BTMAP_Callback-0",
                    new TestUtils.Condition() {
                        @Override
                        public boolean isMet() {
                            return mReceiveCallBack;
                        }
                    }, 5);

            // Receive failed callback.
            assertEquals("Send-0", EmailExternalConstants.RESULT_SUCCESS,
                    mResult);
            mReceiveCallBack = false;

            // 2) Test unavailable eml file
            externalEas = new EmailExternalEas(mContext, mailbox,
                    TestUtils.createUnAvailableUri(), mEmailExternalCalls, true);
            assertNotNull(externalEas);
            thread = new Thread(externalEas, "[BTMAP_Test-1]");
            thread.start();
            TestUtils.sleepAndWait(TestUtils.WAIT_FOR_DB_OPERATION);
            TestUtils.waitUntil("Wait_BTMAP_Callback-1",
                    new TestUtils.Condition() {
                        @Override
                        public boolean isMet() {
                            return mReceiveCallBack;
                        }
                    }, 5);
            // Receive failed callback.
            assertEquals("Send-1", EmailExternalConstants.RESULT_FAIL, mResult);
        } finally {
            EmailContentSetupUtils.deleteTemporaryAccountManagerAccounts(
                    TEST_ACCOUNT_NAME, mProviderContext);
            Configuration.shutDownTest();
            EasResponse.sCallback = null;
        }
    }

    private class ControllerCallbacks extends EmailExternalCalls.Stub {

        public void sendCallback(int result, long accountId, int resultType) {
            mResult = result;
            mReceiveCallBack = true;
        }

        public void updateCallback(int result, long accountId, long mailboxId) {
        }
    }
}
